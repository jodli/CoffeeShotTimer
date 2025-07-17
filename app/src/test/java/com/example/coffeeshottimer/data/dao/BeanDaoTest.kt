package com.example.coffeeshottimer.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.coffeeshottimer.data.database.AppDatabase
import com.example.coffeeshottimer.data.model.Bean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for BeanDao operations.
 * Tests all CRUD operations and queries for Bean entity.
 */
@RunWith(AndroidJUnit4::class)
class BeanDaoTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: AppDatabase
    private lateinit var beanDao: BeanDao
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        beanDao = database.beanDao()
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertBean_insertsSuccessfully() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        
        // When
        beanDao.insertBean(bean)
        
        // Then
        val retrievedBean = beanDao.getBeanById(bean.id)
        assertNotNull("Bean should be inserted", retrievedBean)
        assertEquals("Bean name should match", bean.name, retrievedBean?.name)
        assertEquals("Bean roast date should match", bean.roastDate, retrievedBean?.roastDate)
    }
    
    @Test
    fun insertBean_replacesOnConflict() = runTest {
        // Given
        val originalBean = createTestBean("Original Bean")
        val updatedBean = originalBean.copy(name = "Updated Bean")
        
        // When
        beanDao.insertBean(originalBean)
        beanDao.insertBean(updatedBean) // Should replace due to OnConflictStrategy.REPLACE
        
        // Then
        val retrievedBean = beanDao.getBeanById(originalBean.id)
        assertEquals("Bean name should be updated", "Updated Bean", retrievedBean?.name)
    }
    
    @Test
    fun getBeanById_returnsCorrectBean() = runTest {
        // Given
        val bean1 = createTestBean("Bean 1")
        val bean2 = createTestBean("Bean 2")
        beanDao.insertBean(bean1)
        beanDao.insertBean(bean2)
        
        // When
        val retrievedBean = beanDao.getBeanById(bean1.id)
        
        // Then
        assertNotNull("Bean should be found", retrievedBean)
        assertEquals("Should return correct bean", bean1.name, retrievedBean?.name)
    }
    
    @Test
    fun getBeanById_returnsNullForNonExistentBean() = runTest {
        // When
        val retrievedBean = beanDao.getBeanById("non-existent-id")
        
        // Then
        assertNull("Should return null for non-existent bean", retrievedBean)
    }
    
    @Test
    fun getBeanByName_returnsCorrectBean() = runTest {
        // Given
        val bean = createTestBean("Unique Bean Name")
        beanDao.insertBean(bean)
        
        // When
        val retrievedBean = beanDao.getBeanByName("Unique Bean Name")
        
        // Then
        assertNotNull("Bean should be found by name", retrievedBean)
        assertEquals("Should return correct bean", bean.id, retrievedBean?.id)
    }
    
    @Test
    fun getBeanByName_returnsNullForNonExistentName() = runTest {
        // When
        val retrievedBean = beanDao.getBeanByName("Non-existent Bean")
        
        // Then
        assertNull("Should return null for non-existent bean name", retrievedBean)
    }
    
    @Test
    fun getAllBeans_returnsAllBeansOrderedByCreationDate() = runTest {
        // Given
        val bean1 = createTestBean("Bean 1", createdAt = LocalDateTime.now().minusDays(2))
        val bean2 = createTestBean("Bean 2", createdAt = LocalDateTime.now().minusDays(1))
        val bean3 = createTestBean("Bean 3", createdAt = LocalDateTime.now())
        
        beanDao.insertBean(bean1)
        beanDao.insertBean(bean2)
        beanDao.insertBean(bean3)
        
        // When
        val allBeans = beanDao.getAllBeans().first()
        
        // Then
        assertEquals("Should return all beans", 3, allBeans.size)
        assertEquals("Should be ordered by creation date (newest first)", bean3.name, allBeans[0].name)
        assertEquals("Second bean should be bean2", bean2.name, allBeans[1].name)
        assertEquals("Third bean should be bean1", bean1.name, allBeans[2].name)
    }
    
    @Test
    fun getActiveBeans_returnsOnlyActiveBeans() = runTest {
        // Given
        val activeBean1 = createTestBean("Active Bean 1", isActive = true)
        val activeBean2 = createTestBean("Active Bean 2", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        
        beanDao.insertBean(activeBean1)
        beanDao.insertBean(activeBean2)
        beanDao.insertBean(inactiveBean)
        
        // When
        val activeBeans = beanDao.getActiveBeans().first()
        
        // Then
        assertEquals("Should return only active beans", 2, activeBeans.size)
        assertTrue("Should contain active bean 1", activeBeans.any { it.name == "Active Bean 1" })
        assertTrue("Should contain active bean 2", activeBeans.any { it.name == "Active Bean 2" })
        assertFalse("Should not contain inactive bean", activeBeans.any { it.name == "Inactive Bean" })
    }
    
    @Test
    fun updateBean_updatesSuccessfully() = runTest {
        // Given
        val originalBean = createTestBean("Original Bean")
        beanDao.insertBean(originalBean)
        
        val updatedBean = originalBean.copy(
            name = "Updated Bean",
            notes = "Updated notes"
        )
        
        // When
        beanDao.updateBean(updatedBean)
        
        // Then
        val retrievedBean = beanDao.getBeanById(originalBean.id)
        assertEquals("Bean name should be updated", "Updated Bean", retrievedBean?.name)
        assertEquals("Bean notes should be updated", "Updated notes", retrievedBean?.notes)
    }
    
    @Test
    fun deleteBean_deletesSuccessfully() = runTest {
        // Given
        val bean = createTestBean("Bean to Delete")
        beanDao.insertBean(bean)
        
        // Verify bean exists
        assertNotNull("Bean should exist before deletion", beanDao.getBeanById(bean.id))
        
        // When
        beanDao.deleteBean(bean)
        
        // Then
        assertNull("Bean should be deleted", beanDao.getBeanById(bean.id))
    }
    
    @Test
    fun updateLastGrinderSetting_updatesCorrectly() = runTest {
        // Given
        val bean = createTestBean("Test Bean")
        beanDao.insertBean(bean)
        
        // When
        beanDao.updateLastGrinderSetting(bean.id, "15.5")
        
        // Then
        val updatedBean = beanDao.getBeanById(bean.id)
        assertEquals("Grinder setting should be updated", "15.5", updatedBean?.lastGrinderSetting)
    }
    
    @Test
    fun updateBeanActiveStatus_updatesCorrectly() = runTest {
        // Given
        val bean = createTestBean("Test Bean", isActive = true)
        beanDao.insertBean(bean)
        
        // When
        beanDao.updateBeanActiveStatus(bean.id, false)
        
        // Then
        val updatedBean = beanDao.getBeanById(bean.id)
        assertFalse("Bean should be inactive", updatedBean?.isActive ?: true)
    }
    
    @Test
    fun getFilteredBeans_filtersActiveOnly() = runTest {
        // Given
        val activeBean = createTestBean("Active Bean", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        
        beanDao.insertBean(activeBean)
        beanDao.insertBean(inactiveBean)
        
        // When - filter active only
        val activeBeans = beanDao.getFilteredBeans(activeOnly = true, searchQuery = "").first()
        
        // Then
        assertEquals("Should return only active beans", 1, activeBeans.size)
        assertEquals("Should return the active bean", activeBean.name, activeBeans[0].name)
    }
    
    @Test
    fun getFilteredBeans_filtersAllBeans() = runTest {
        // Given
        val activeBean = createTestBean("Active Bean", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        
        beanDao.insertBean(activeBean)
        beanDao.insertBean(inactiveBean)
        
        // When - don't filter active only
        val allBeans = beanDao.getFilteredBeans(activeOnly = false, searchQuery = "").first()
        
        // Then
        assertEquals("Should return all beans", 2, allBeans.size)
    }
    
    @Test
    fun getFilteredBeans_filtersBySearchQuery() = runTest {
        // Given
        val bean1 = createTestBean("Ethiopian Coffee")
        val bean2 = createTestBean("Colombian Coffee")
        val bean3 = createTestBean("Brazilian Espresso")
        
        beanDao.insertBean(bean1)
        beanDao.insertBean(bean2)
        beanDao.insertBean(bean3)
        
        // When - search for "Coffee"
        val coffeBeans = beanDao.getFilteredBeans(activeOnly = false, searchQuery = "Coffee").first()
        
        // Then
        assertEquals("Should return beans containing 'Coffee'", 2, coffeBeans.size)
        assertTrue("Should contain Ethiopian Coffee", coffeBeans.any { it.name == "Ethiopian Coffee" })
        assertTrue("Should contain Colombian Coffee", coffeBeans.any { it.name == "Colombian Coffee" })
        assertFalse("Should not contain Brazilian Espresso", coffeBeans.any { it.name == "Brazilian Espresso" })
    }
    
    @Test
    fun getFilteredBeans_combinesActiveAndSearchFilters() = runTest {
        // Given
        val activeBean1 = createTestBean("Active Coffee Bean", isActive = true)
        val activeBean2 = createTestBean("Active Espresso Bean", isActive = true)
        val inactiveBean = createTestBean("Inactive Coffee Bean", isActive = false)
        
        beanDao.insertBean(activeBean1)
        beanDao.insertBean(activeBean2)
        beanDao.insertBean(inactiveBean)
        
        // When - filter active beans containing "Coffee"
        val filteredBeans = beanDao.getFilteredBeans(activeOnly = true, searchQuery = "Coffee").first()
        
        // Then
        assertEquals("Should return only active beans containing 'Coffee'", 1, filteredBeans.size)
        assertEquals("Should return the active coffee bean", activeBean1.name, filteredBeans[0].name)
    }
    
    @Test
    fun getActiveBeanCount_returnsCorrectCount() = runTest {
        // Given
        val activeBean1 = createTestBean("Active Bean 1", isActive = true)
        val activeBean2 = createTestBean("Active Bean 2", isActive = true)
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        
        beanDao.insertBean(activeBean1)
        beanDao.insertBean(activeBean2)
        beanDao.insertBean(inactiveBean)
        
        // When
        val count = beanDao.getActiveBeanCount()
        
        // Then
        assertEquals("Should return correct count of active beans", 2, count)
    }
    
    @Test
    fun getActiveBeanCount_returnsZeroWhenNoActiveBeans() = runTest {
        // Given
        val inactiveBean = createTestBean("Inactive Bean", isActive = false)
        beanDao.insertBean(inactiveBean)
        
        // When
        val count = beanDao.getActiveBeanCount()
        
        // Then
        assertEquals("Should return zero when no active beans", 0, count)
    }
    
    /**
     * Helper function to create test beans with default values.
     */
    private fun createTestBean(
        name: String,
        roastDate: LocalDate = LocalDate.now().minusDays(7),
        notes: String = "Test notes",
        isActive: Boolean = true,
        lastGrinderSetting: String? = null,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Bean {
        return Bean(
            name = name,
            roastDate = roastDate,
            notes = notes,
            isActive = isActive,
            lastGrinderSetting = lastGrinderSetting,
            createdAt = createdAt
        )
    }
}