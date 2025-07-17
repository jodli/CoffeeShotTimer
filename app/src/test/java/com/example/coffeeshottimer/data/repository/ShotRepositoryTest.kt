package com.example.coffeeshottimer.data.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.coffeeshottimer.data.database.AppDatabase
import com.example.coffeeshottimer.data.model.Bean
import com.example.coffeeshottimer.data.model.Shot
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
import java.time.LocalDateTime

/**
 * Integration tests for ShotRepository.
 * Tests repository operations with real database interactions.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ShotRepositoryTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: AppDatabase
    private lateinit var shotRepository: ShotRepository
    private lateinit var beanRepository: BeanRepository
    private lateinit var testBean: Bean
    
    @Before
    fun setup() = runTest {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        shotRepository = ShotRepository(database.shotDao(), database.beanDao())
        beanRepository = BeanRepository(database.beanDao())
        
        // Create a test bean for shots
        testBean = createTestBean("Test Bean")
        beanRepository.addBean(testBean)
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun recordShot_validShot_succeeds() = runTest {
        // Given
        val shot = createTestShot(testBean.id)
        
        // When
        val result = shotRepository.recordShot(shot)
        
        // Then
        assertTrue("Recording valid shot should succeed", result.isSuccess)
        
        val retrievedShot = shotRepository.getShotById(shot.id).getOrNull()
        assertNotNull("Shot should be retrievable after recording", retrievedShot)
        assertEquals("Shot coffee weight in should match", shot.coffeeWeightIn, retrievedShot?.coffeeWeightIn)
        assertEquals("Shot coffee weight out should match", shot.coffeeWeightOut, retrievedShot?.coffeeWeightOut)
    }
    
    @Test
    fun recordShot_invalidShot_fails() = runTest {
        // Given
        val invalidShot = createTestShot(testBean.id, coffeeWeightIn = -1.0) // Invalid weight
        
        // When
        val result = shotRepository.recordShot(invalidShot)
        
        // Then
        assertTrue("Recording invalid shot should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun recordShot_nonExistentBean_fails() = runTest {
        // Given
        val shotWithInvalidBean = createTestShot("non-existent-bean-id")
        
        // When
        val result = shotRepository.recordShot(shotWithInvalidBean)
        
        // Then
        assertTrue("Recording shot with non-existent bean should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun recordShot_updatesLastGrinderSetting() = runTest {
        // Given
        val shot = createTestShot(testBean.id, grinderSetting = "16.5")
        
        // When
        val result = shotRepository.recordShot(shot)
        
        // Then
        assertTrue("Recording shot should succeed", result.isSuccess)
        
        val updatedBean = beanRepository.getBeanById(testBean.id).getOrNull()
        assertEquals("Bean's last grinder setting should be updated", "16.5", updatedBean?.lastGrinderSetting)
    }
    
    @Test
    fun updateShot_validShot_succeeds() = runTest {
        // Given
        val originalShot = createTestShot(testBean.id)
        shotRepository.recordShot(originalShot)
        
        val updatedShot = originalShot.copy(coffeeWeightOut = 45.0, notes = "Updated notes")
        
        // When
        val result = shotRepository.updateShot(updatedShot)
        
        // Then
        assertTrue("Updating valid shot should succeed", result.isSuccess)
        
        val retrievedShot = shotRepository.getShotById(originalShot.id).getOrNull()
        assertEquals("Shot weight out should be updated", 45.0, retrievedShot?.coffeeWeightOut)
        assertEquals("Shot notes should be updated", "Updated notes", retrievedShot?.notes)
    }
    
    @Test
    fun updateShot_nonExistentShot_fails() = runTest {
        // Given
        val nonExistentShot = createTestShot(testBean.id)
        
        // When
        val result = shotRepository.updateShot(nonExistentShot)
        
        // Then
        assertTrue("Updating non-existent shot should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }
    
    @Test
    fun deleteShot_existingShot_succeeds() = runTest {
        // Given
        val shot = createTestShot(testBean.id)
        shotRepository.recordShot(shot)
        
        // When
        val result = shotRepository.deleteShot(shot)
        
        // Then
        assertTrue("Deleting existing shot should succeed", result.isSuccess)
        
        val retrievedShot = shotRepository.getShotById(shot.id).getOrNull()
        assertNull("Shot should not be retrievable after deletion", retrievedShot)
    }
    
    @Test
    fun deleteShot_nonExistentShot_fails() = runTest {
        // Given
        val nonExistentShot = createTestShot(testBean.id)
        
        // When
        val result = shotRepository.deleteShot(nonExistentShot)
        
        // Then
        assertTrue("Deleting non-existent shot should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }
    
    @Test
    fun getShotById_existingShot_returnsShot() = runTest {
        // Given
        val shot = createTestShot(testBean.id)
        shotRepository.recordShot(shot)
        
        // When
        val result = shotRepository.getShotById(shot.id)
        
        // Then
        assertTrue("Getting existing shot should succeed", result.isSuccess)
        assertEquals("Should return correct shot", shot.coffeeWeightIn, result.getOrNull()?.coffeeWeightIn)
    }
    
    @Test
    fun getShotById_nonExistentShot_returnsNull() = runTest {
        // When
        val result = shotRepository.getShotById("non-existent-id")
        
        // Then
        assertTrue("Getting non-existent shot should succeed but return null", result.isSuccess)
        assertNull("Should return null for non-existent shot", result.getOrNull())
    }
    
    @Test
    fun getShotById_emptyId_fails() = runTest {
        // When
        val result = shotRepository.getShotById("")
        
        // Then
        assertTrue("Getting shot with empty ID should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun getAllShots_returnsAllShots() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, coffeeWeightIn = 18.0)
        val shot2 = createTestShot(testBean.id, coffeeWeightIn = 19.0)
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        
        // When
        val result = shotRepository.getAllShots().first()
        
        // Then
        assertTrue("Getting all shots should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return all shots", 2, shots?.size)
        assertTrue("Should contain shot 1", shots?.any { it.coffeeWeightIn == 18.0 } == true)
        assertTrue("Should contain shot 2", shots?.any { it.coffeeWeightIn == 19.0 } == true)
    }
    
    @Test
    fun getShotsByBean_returnsCorrectShots() = runTest {
        // Given
        val anotherBean = createTestBean("Another Bean")
        beanRepository.addBean(anotherBean)
        
        val shotForTestBean = createTestShot(testBean.id, coffeeWeightIn = 18.0)
        val shotForAnotherBean = createTestShot(anotherBean.id, coffeeWeightIn = 19.0)
        
        shotRepository.recordShot(shotForTestBean)
        shotRepository.recordShot(shotForAnotherBean)
        
        // When
        val result = shotRepository.getShotsByBean(testBean.id).first()
        
        // Then
        assertTrue("Getting shots by bean should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return only shots for the specified bean", 1, shots?.size)
        assertEquals("Should return the correct shot", 18.0, shots?.first()?.coffeeWeightIn)
    }
    
    @Test
    fun getShotsByBean_emptyBeanId_fails() = runTest {
        // When
        val result = shotRepository.getShotsByBean("").first()
        
        // Then
        assertTrue("Getting shots with empty bean ID should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun getRecentShots_returnsLimitedShots() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, timestamp = LocalDateTime.now().minusHours(3))
        val shot2 = createTestShot(testBean.id, timestamp = LocalDateTime.now().minusHours(2))
        val shot3 = createTestShot(testBean.id, timestamp = LocalDateTime.now().minusHours(1))
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When
        val result = shotRepository.getRecentShots(2).first()
        
        // Then
        assertTrue("Getting recent shots should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return limited number of shots", 2, shots?.size)
        // Should return the most recent shots (shot3 and shot2)
        assertTrue("Should contain most recent shots", 
            shots?.any { it.timestamp.hour == shot3.timestamp.hour } == true)
    }
    
    @Test
    fun getRecentShots_invalidLimit_fails() = runTest {
        // When
        val result = shotRepository.getRecentShots(0).first()
        
        // Then
        assertTrue("Getting recent shots with invalid limit should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun getShotsByDateRange_returnsCorrectShots() = runTest {
        // Given
        val now = LocalDateTime.now()
        val shot1 = createTestShot(testBean.id, timestamp = now.minusDays(3))
        val shot2 = createTestShot(testBean.id, timestamp = now.minusDays(1))
        val shot3 = createTestShot(testBean.id, timestamp = now.plusDays(1))
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When
        val result = shotRepository.getShotsByDateRange(
            now.minusDays(2), 
            now
        ).first()
        
        // Then
        assertTrue("Getting shots by date range should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return only shots in date range", 1, shots?.size)
        assertEquals("Should return shot2", shot2.id, shots?.first()?.id)
    }
    
    @Test
    fun getShotsByDateRange_invalidRange_fails() = runTest {
        // Given
        val now = LocalDateTime.now()
        
        // When
        val result = shotRepository.getShotsByDateRange(now, now.minusDays(1)).first()
        
        // Then
        assertTrue("Getting shots with invalid date range should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun getFilteredShots_filtersCorrectly() = runTest {
        // Given
        val anotherBean = createTestBean("Another Bean")
        beanRepository.addBean(anotherBean)
        
        val now = LocalDateTime.now()
        val shot1 = createTestShot(testBean.id, timestamp = now.minusDays(1))
        val shot2 = createTestShot(anotherBean.id, timestamp = now.minusDays(1))
        val shot3 = createTestShot(testBean.id, timestamp = now.minusDays(3))
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When - filter by bean and date range
        val result = shotRepository.getFilteredShots(
            beanId = testBean.id,
            startDate = now.minusDays(2),
            endDate = now
        ).first()
        
        // Then
        assertTrue("Getting filtered shots should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return only shots matching filters", 1, shots?.size)
        assertEquals("Should return shot1", shot1.id, shots?.first()?.id)
    }
    
    @Test
    fun getShotsByGrinderSetting_returnsCorrectShots() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, grinderSetting = "15.5")
        val shot2 = createTestShot(testBean.id, grinderSetting = "16.0")
        val shot3 = createTestShot(testBean.id, grinderSetting = "15.5")
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When
        val result = shotRepository.getShotsByGrinderSetting("15.5").first()
        
        // Then
        assertTrue("Getting shots by grinder setting should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return shots with matching grinder setting", 2, shots?.size)
        assertTrue("All shots should have correct grinder setting", 
            shots?.all { it.grinderSetting == "15.5" } == true)
    }
    
    @Test
    fun getShotsByGrinderSetting_emptyGrinderSetting_fails() = runTest {
        // When
        val result = shotRepository.getShotsByGrinderSetting("").first()
        
        // Then
        assertTrue("Getting shots with empty grinder setting should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun deleteShotsByBean_deletesCorrectShots() = runTest {
        // Given
        val anotherBean = createTestBean("Another Bean")
        beanRepository.addBean(anotherBean)
        
        val shotForTestBean = createTestShot(testBean.id)
        val shotForAnotherBean = createTestShot(anotherBean.id)
        
        shotRepository.recordShot(shotForTestBean)
        shotRepository.recordShot(shotForAnotherBean)
        
        // When
        val result = shotRepository.deleteShotsByBean(testBean.id)
        
        // Then
        assertTrue("Deleting shots by bean should succeed", result.isSuccess)
        
        val remainingShots = shotRepository.getAllShots().first().getOrNull()
        assertEquals("Should only have shots for other beans", 1, remainingShots?.size)
        assertEquals("Remaining shot should be for another bean", anotherBean.id, remainingShots?.first()?.beanId)
    }
    
    @Test
    fun getShotStatistics_returnsCorrectStatistics() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, coffeeWeightIn = 18.0, coffeeWeightOut = 36.0, extractionTimeSeconds = 25)
        val shot2 = createTestShot(testBean.id, coffeeWeightIn = 19.0, coffeeWeightOut = 38.0, extractionTimeSeconds = 30)
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        
        // When
        val result = shotRepository.getShotStatistics(testBean.id)
        
        // Then
        assertTrue("Getting shot statistics should succeed", result.isSuccess)
        val statistics = result.getOrNull()
        assertNotNull("Statistics should not be null", statistics)
        assertEquals("Should have correct shot count", 2, statistics?.shotCount)
        assertEquals("Should have correct average input weight", 18.5, statistics?.avgCoffeeWeightIn, 0.1)
        assertEquals("Should have correct average output weight", 37.0, statistics?.avgCoffeeWeightOut, 0.1)
        assertEquals("Should have correct average extraction time", 27.5, statistics?.avgExtractionTimeSeconds, 0.1)
    }
    
    @Test
    fun getLastShotForBean_returnsCorrectShot() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, timestamp = LocalDateTime.now().minusHours(2))
        val shot2 = createTestShot(testBean.id, timestamp = LocalDateTime.now().minusHours(1))
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        
        // When
        val result = shotRepository.getLastShotForBean(testBean.id)
        
        // Then
        assertTrue("Getting last shot for bean should succeed", result.isSuccess)
        val lastShot = result.getOrNull()
        assertEquals("Should return the most recent shot", shot2.id, lastShot?.id)
    }
    
    @Test
    fun getTotalShotCount_returnsCorrectCount() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id)
        val shot2 = createTestShot(testBean.id)
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        
        // When
        val result = shotRepository.getTotalShotCount()
        
        // Then
        assertTrue("Getting total shot count should succeed", result.isSuccess)
        assertEquals("Should return correct total count", 2, result.getOrNull())
    }
    
    @Test
    fun getShotsByBrewRatioRange_returnsCorrectShots() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, coffeeWeightIn = 18.0, coffeeWeightOut = 36.0) // ratio 2.0
        val shot2 = createTestShot(testBean.id, coffeeWeightIn = 18.0, coffeeWeightOut = 45.0) // ratio 2.5
        val shot3 = createTestShot(testBean.id, coffeeWeightIn = 18.0, coffeeWeightOut = 54.0) // ratio 3.0
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When
        val result = shotRepository.getShotsByBrewRatioRange(2.0, 2.5).first()
        
        // Then
        assertTrue("Getting shots by brew ratio range should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return shots within brew ratio range", 2, shots?.size)
        assertTrue("Should contain shot with ratio 2.0", shots?.any { it.brewRatio == 2.0 } == true)
        assertTrue("Should contain shot with ratio 2.5", shots?.any { it.brewRatio == 2.5 } == true)
    }
    
    @Test
    fun getShotsByExtractionTimeRange_returnsCorrectShots() = runTest {
        // Given
        val shot1 = createTestShot(testBean.id, extractionTimeSeconds = 20)
        val shot2 = createTestShot(testBean.id, extractionTimeSeconds = 25)
        val shot3 = createTestShot(testBean.id, extractionTimeSeconds = 35)
        
        shotRepository.recordShot(shot1)
        shotRepository.recordShot(shot2)
        shotRepository.recordShot(shot3)
        
        // When
        val result = shotRepository.getShotsByExtractionTimeRange(22, 30).first()
        
        // Then
        assertTrue("Getting shots by extraction time range should succeed", result.isSuccess)
        val shots = result.getOrNull()
        assertEquals("Should return shots within extraction time range", 1, shots?.size)
        assertEquals("Should return shot with 25 seconds", 25, shots?.first()?.extractionTimeSeconds)
    }
    
    @Test
    fun validateShot_validShot_returnsValid() = runTest {
        // Given
        val validShot = createTestShot(testBean.id)
        
        // When
        val validationResult = shotRepository.validateShot(validShot)
        
        // Then
        assertTrue("Valid shot should pass validation", validationResult.isValid)
        assertTrue("Valid shot should have no errors", validationResult.errors.isEmpty())
    }
    
    @Test
    fun validateShot_invalidShot_returnsInvalid() = runTest {
        // Given
        val invalidShot = createTestShot(testBean.id, coffeeWeightIn = -1.0) // Invalid weight
        
        // When
        val validationResult = shotRepository.validateShot(invalidShot)
        
        // Then
        assertFalse("Invalid shot should fail validation", validationResult.isValid)
        assertFalse("Invalid shot should have errors", validationResult.errors.isEmpty())
    }
    
    @Test
    fun validateShot_nonExistentBean_returnsInvalid() = runTest {
        // Given
        val shotWithInvalidBean = createTestShot("non-existent-bean-id")
        
        // When
        val validationResult = shotRepository.validateShot(shotWithInvalidBean)
        
        // Then
        assertFalse("Shot with non-existent bean should fail validation", validationResult.isValid)
        assertTrue("Should have bean existence error", 
            validationResult.errors.any { it.contains("does not exist") })
    }
    
    @Test
    fun getSuggestedGrinderSetting_fromBeanLastSetting_returnsCorrectSetting() = runTest {
        // Given
        beanRepository.updateLastGrinderSetting(testBean.id, "16.5")
        
        // When
        val result = shotRepository.getSuggestedGrinderSetting(testBean.id)
        
        // Then
        assertTrue("Getting suggested grinder setting should succeed", result.isSuccess)
        assertEquals("Should return bean's last grinder setting", "16.5", result.getOrNull())
    }
    
    @Test
    fun getSuggestedGrinderSetting_fromLastShot_returnsCorrectSetting() = runTest {
        // Given
        val shot = createTestShot(testBean.id, grinderSetting = "17.0")
        shotRepository.recordShot(shot)
        
        // When
        val result = shotRepository.getSuggestedGrinderSetting(testBean.id)
        
        // Then
        assertTrue("Getting suggested grinder setting should succeed", result.isSuccess)
        assertEquals("Should return last shot's grinder setting", "17.0", result.getOrNull())
    }
    
    @Test
    fun getSuggestedGrinderSetting_noHistory_returnsNull() = runTest {
        // Given - no shots or grinder settings for the bean
        
        // When
        val result = shotRepository.getSuggestedGrinderSetting(testBean.id)
        
        // Then
        assertTrue("Getting suggested grinder setting should succeed", result.isSuccess)
        assertNull("Should return null when no history exists", result.getOrNull())
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
    
    /**
     * Helper function to create test shots with default values.
     */
    private fun createTestShot(
        beanId: String,
        coffeeWeightIn: Double = 18.0,
        coffeeWeightOut: Double = 36.0,
        extractionTimeSeconds: Int = 25,
        grinderSetting: String = "15.5",
        notes: String = "Test shot",
        timestamp: LocalDateTime = LocalDateTime.now()
    ): Shot {
        return Shot(
            beanId = beanId,
            coffeeWeightIn = coffeeWeightIn,
            coffeeWeightOut = coffeeWeightOut,
            extractionTimeSeconds = extractionTimeSeconds,
            grinderSetting = grinderSetting,
            notes = notes,
            timestamp = timestamp
        )
    }
}