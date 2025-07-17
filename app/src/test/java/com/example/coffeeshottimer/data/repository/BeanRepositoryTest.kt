package com.example.coffeeshottimer.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.coffeeshottimer.data.database.AppDatabase
import com.example.coffeeshottimer.data.model.Bean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.annotation.Config
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

/**
 * Integration tests for BeanRepository.
 * Tests repository operations with real database interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BeanRepositoryTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: AppDatabase
    private lateinit var repository: BeanRepository
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        repository = BeanRepository(database.beanDao())
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
        assertTrue("Should have uniqueness error", 
            validationResult.errors.any { it.contains("already exists") })
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