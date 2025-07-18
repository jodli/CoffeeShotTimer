package com.jodli.coffeeshottimer.data.dao

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.Bean
import com.jodli.coffeeshottimer.data.model.Shot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit tests for ShotDao operations.
 * Tests all CRUD operations and queries for Shot entity.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ShotDaoTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    private lateinit var database: AppDatabase
    private lateinit var shotDao: ShotDao
    private lateinit var beanDao: BeanDao
    
    // Test data
    private lateinit var testBean1: Bean
    private lateinit var testBean2: Bean
    
    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        shotDao = database.shotDao()
        beanDao = database.beanDao()
        
        // Create test beans
        testBean1 = Bean(
            id = "bean-1",
            name = "Test Bean 1",
            roastDate = LocalDate.now().minusDays(7)
        )
        testBean2 = Bean(
            id = "bean-2",
            name = "Test Bean 2",
            roastDate = LocalDate.now().minusDays(5)
        )
    }
    
    @After
    fun teardown() {
        database.close()
    }
    
    @Test
    fun insertShot_insertsSuccessfully() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot = createTestShot(beanId = testBean1.id)
        
        // When
        shotDao.insertShot(shot)
        
        // Then
        val retrievedShot = shotDao.getShotById(shot.id)
        assertNotNull("Shot should be inserted", retrievedShot)
        assertEquals("Shot bean ID should match", shot.beanId, retrievedShot?.beanId)
        assertEquals("Shot coffee weight in should match", shot.coffeeWeightIn, retrievedShot?.coffeeWeightIn ?: 0.0, 0.01)
        assertEquals("Shot coffee weight out should match", shot.coffeeWeightOut, retrievedShot?.coffeeWeightOut ?: 0.0, 0.01)
    }
    
    @Test
    fun insertShot_replacesOnConflict() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val originalShot = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val updatedShot = originalShot.copy(grinderSetting = "16")
        
        // When
        shotDao.insertShot(originalShot)
        shotDao.insertShot(updatedShot) // Should replace due to OnConflictStrategy.REPLACE
        
        // Then
        val retrievedShot = shotDao.getShotById(originalShot.id)
        assertEquals("Grinder setting should be updated", "16", retrievedShot?.grinderSetting)
    }
    
    @Test
    fun getShotById_returnsCorrectShot() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot1 = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val shot2 = createTestShot(beanId = testBean1.id, grinderSetting = "16")
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        
        // When
        val retrievedShot = shotDao.getShotById(shot1.id)
        
        // Then
        assertNotNull("Shot should be found", retrievedShot)
        assertEquals("Should return correct shot", shot1.grinderSetting, retrievedShot?.grinderSetting)
    }
    
    @Test
    fun getShotById_returnsNullForNonExistentShot() = runTest {
        // When
        val retrievedShot = shotDao.getShotById("non-existent-id")
        
        // Then
        assertNull("Should return null for non-existent shot", retrievedShot)
    }
    
    @Test
    fun getAllShots_returnsAllShotsOrderedByTimestamp() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot1 = createTestShot(
            beanId = testBean1.id,
            grinderSetting = "15",
            timestamp = LocalDateTime.now().minusHours(2)
        )
        val shot2 = createTestShot(
            beanId = testBean1.id,
            grinderSetting = "16",
            timestamp = LocalDateTime.now().minusHours(1)
        )
        val shot3 = createTestShot(
            beanId = testBean1.id,
            grinderSetting = "17",
            timestamp = LocalDateTime.now()
        )
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When
        val allShots = shotDao.getAllShots().first()
        
        // Then
        assertEquals("Should return all shots", 3, allShots.size)
        assertEquals("Should be ordered by timestamp (newest first)", shot3.grinderSetting, allShots[0].grinderSetting)
        assertEquals("Second shot should be shot2", shot2.grinderSetting, allShots[1].grinderSetting)
        assertEquals("Third shot should be shot1", shot1.grinderSetting, allShots[2].grinderSetting)
    }
    
    @Test
    fun getShotsByBean_returnsOnlyShotsForSpecificBean() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        beanDao.insertBean(testBean2)
        
        val shot1Bean1 = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val shot2Bean1 = createTestShot(beanId = testBean1.id, grinderSetting = "16")
        val shot1Bean2 = createTestShot(beanId = testBean2.id, grinderSetting = "20")
        
        shotDao.insertShot(shot1Bean1)
        shotDao.insertShot(shot2Bean1)
        shotDao.insertShot(shot1Bean2)
        
        // When
        val bean1Shots = shotDao.getShotsByBean(testBean1.id).first()
        
        // Then
        assertEquals("Should return shots for bean 1 only", 2, bean1Shots.size)
        assertTrue("Should contain shot1Bean1", bean1Shots.any { it.grinderSetting == "15" })
        assertTrue("Should contain shot2Bean1", bean1Shots.any { it.grinderSetting == "16" })
        assertFalse("Should not contain shot from bean 2", bean1Shots.any { it.grinderSetting == "20" })
    }
    
    @Test
    fun getRecentShots_returnsLimitedNumberOfShots() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        // Create 5 shots
        for (i in 1..5) {
            val shot = createTestShot(
                beanId = testBean1.id,
                grinderSetting = i.toString(),
                timestamp = LocalDateTime.now().minusHours(i.toLong())
            )
            shotDao.insertShot(shot)
        }
        
        // When
        val recentShots = shotDao.getRecentShots(3).first()
        
        // Then
        assertEquals("Should return only 3 recent shots", 3, recentShots.size)
        assertEquals("Should return newest shot first", "1", recentShots[0].grinderSetting)
        assertEquals("Should return second newest shot", "2", recentShots[1].grinderSetting)
        assertEquals("Should return third newest shot", "3", recentShots[2].grinderSetting)
    }
    
    @Test
    fun getShotsByDateRange_returnsOnlyShotsInRange() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        val now = LocalDateTime.now()
        val shot1 = createTestShot(beanId = testBean1.id, grinderSetting = "15", timestamp = now.minusDays(3))
        val shot2 = createTestShot(beanId = testBean1.id, grinderSetting = "16", timestamp = now.minusDays(1))
        val shot3 = createTestShot(beanId = testBean1.id, grinderSetting = "17", timestamp = now)
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When - get shots from 2 days ago to now
        val shotsInRange = shotDao.getShotsByDateRange(now.minusDays(2), now).first()
        
        // Then
        assertEquals("Should return shots in date range", 2, shotsInRange.size)
        assertTrue("Should contain shot2", shotsInRange.any { it.grinderSetting == "16" })
        assertTrue("Should contain shot3", shotsInRange.any { it.grinderSetting == "17" })
        assertFalse("Should not contain shot1", shotsInRange.any { it.grinderSetting == "15" })
    }
    
    @Test
    fun getFilteredShots_filtersCorrectly() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        beanDao.insertBean(testBean2)
        
        val now = LocalDateTime.now()
        val shot1 = createTestShot(beanId = testBean1.id, grinderSetting = "15", timestamp = now.minusDays(1))
        val shot2 = createTestShot(beanId = testBean1.id, grinderSetting = "16", timestamp = now)
        val shot3 = createTestShot(beanId = testBean2.id, grinderSetting = "20", timestamp = now.minusHours(1))
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When - filter by bean1 and last 12 hours
        val filteredShots = shotDao.getFilteredShots(
            beanId = testBean1.id,
            startDate = now.minusHours(12),
            endDate = now
        ).first()
        
        // Then
        assertEquals("Should return filtered shots", 1, filteredShots.size)
        assertEquals("Should return shot2", "16", filteredShots[0].grinderSetting)
    }
    
    @Test
    fun getFilteredShots_handlesNullFilters() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        shotDao.insertShot(shot)
        
        // When - no filters applied
        val allShots = shotDao.getFilteredShots(
            beanId = null,
            startDate = null,
            endDate = null
        ).first()
        
        // Then
        assertEquals("Should return all shots when no filters", 1, allShots.size)
    }
    
    @Test
    fun getShotsByGrinderSetting_returnsCorrectShots() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot1 = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val shot2 = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val shot3 = createTestShot(beanId = testBean1.id, grinderSetting = "16")
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When
        val shotsWithSetting15 = shotDao.getShotsByGrinderSetting("15").first()
        
        // Then
        assertEquals("Should return shots with grinder setting 15", 2, shotsWithSetting15.size)
        assertTrue("All shots should have grinder setting 15", 
            shotsWithSetting15.all { it.grinderSetting == "15" })
    }
    
    @Test
    fun updateShot_updatesSuccessfully() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val originalShot = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        shotDao.insertShot(originalShot)
        
        val updatedShot = originalShot.copy(
            grinderSetting = "16",
            notes = "Updated notes"
        )
        
        // When
        shotDao.updateShot(updatedShot)
        
        // Then
        val retrievedShot = shotDao.getShotById(originalShot.id)
        assertEquals("Grinder setting should be updated", "16", retrievedShot?.grinderSetting)
        assertEquals("Notes should be updated", "Updated notes", retrievedShot?.notes)
    }
    
    @Test
    fun deleteShot_deletesSuccessfully() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        val shot = createTestShot(beanId = testBean1.id)
        shotDao.insertShot(shot)
        
        // Verify shot exists
        assertNotNull("Shot should exist before deletion", shotDao.getShotById(shot.id))
        
        // When
        shotDao.deleteShot(shot)
        
        // Then
        assertNull("Shot should be deleted", shotDao.getShotById(shot.id))
    }
    
    @Test
    fun deleteShotsByBean_deletesAllShotsForBean() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        beanDao.insertBean(testBean2)
        
        val shot1Bean1 = createTestShot(beanId = testBean1.id, grinderSetting = "15")
        val shot2Bean1 = createTestShot(beanId = testBean1.id, grinderSetting = "16")
        val shot1Bean2 = createTestShot(beanId = testBean2.id, grinderSetting = "20")
        
        shotDao.insertShot(shot1Bean1)
        shotDao.insertShot(shot2Bean1)
        shotDao.insertShot(shot1Bean2)
        
        // When
        shotDao.deleteShotsByBean(testBean1.id)
        
        // Then
        val remainingShots = shotDao.getAllShots().first()
        assertEquals("Should have only 1 shot remaining", 1, remainingShots.size)
        assertEquals("Remaining shot should be from bean2", testBean2.id, remainingShots[0].beanId)
    }
    
    @Test
    fun getShotStatistics_calculatesCorrectly() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        val shot1 = createTestShot(
            beanId = testBean1.id,
            coffeeWeightIn = 18.0,
            coffeeWeightOut = 36.0,
            extractionTimeSeconds = 25
        )
        val shot2 = createTestShot(
            beanId = testBean1.id,
            coffeeWeightIn = 20.0,
            coffeeWeightOut = 40.0,
            extractionTimeSeconds = 30
        )
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        
        // When
        val statistics = shotDao.getShotStatistics(testBean1.id)
        
        // Then
        assertNotNull("Statistics should be calculated", statistics)
        assertEquals("Total shots should be 2", 2, statistics?.totalShots)
        assertEquals("Average weight in should be 19.0", 19.0, statistics?.avgWeightIn ?: 0.0, 0.01)
        assertEquals("Average weight out should be 38.0", 38.0, statistics?.avgWeightOut ?: 0.0, 0.01)
        assertEquals("Average extraction time should be 27.5", 27.5, statistics?.avgExtractionTime ?: 0.0, 0.01)
        assertEquals("Average brew ratio should be 2.0", 2.0, statistics?.avgBrewRatio ?: 0.0, 0.01)
    }
    
    @Test
    fun getLastShotForBean_returnsNewestShot() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        val shot1 = createTestShot(
            beanId = testBean1.id,
            grinderSetting = "15",
            timestamp = LocalDateTime.now().minusHours(2)
        )
        val shot2 = createTestShot(
            beanId = testBean1.id,
            grinderSetting = "16",
            timestamp = LocalDateTime.now()
        )
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        
        // When
        val lastShot = shotDao.getLastShotForBean(testBean1.id)
        
        // Then
        assertNotNull("Last shot should be found", lastShot)
        assertEquals("Should return the newest shot", "16", lastShot?.grinderSetting)
    }
    
    @Test
    fun getTotalShotCount_returnsCorrectCount() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        beanDao.insertBean(testBean2)
        
        val shot1 = createTestShot(beanId = testBean1.id)
        val shot2 = createTestShot(beanId = testBean1.id)
        val shot3 = createTestShot(beanId = testBean2.id)
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When
        val count = shotDao.getTotalShotCount()
        
        // Then
        assertEquals("Should return total shot count", 3, count)
    }
    
    @Test
    fun getShotsByBrewRatioRange_filtersCorrectly() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        val shot1 = createTestShot(beanId = testBean1.id, coffeeWeightIn = 18.0, coffeeWeightOut = 27.0) // 1:1.5
        val shot2 = createTestShot(beanId = testBean1.id, coffeeWeightIn = 18.0, coffeeWeightOut = 36.0) // 1:2.0
        val shot3 = createTestShot(beanId = testBean1.id, coffeeWeightIn = 18.0, coffeeWeightOut = 54.0) // 1:3.0
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When - get shots with brew ratio between 1.8 and 2.2
        val shotsInRange = shotDao.getShotsByBrewRatioRange(1.8, 2.2).first()
        
        // Then
        assertEquals("Should return shots in brew ratio range", 1, shotsInRange.size)
        assertEquals("Should return shot with 1:2 ratio", 36.0, shotsInRange[0].coffeeWeightOut, 0.01)
    }
    
    @Test
    fun getShotsByExtractionTimeRange_filtersCorrectly() = runTest {
        // Given
        beanDao.insertBean(testBean1)
        
        val shot1 = createTestShot(beanId = testBean1.id, extractionTimeSeconds = 20)
        val shot2 = createTestShot(beanId = testBean1.id, extractionTimeSeconds = 25)
        val shot3 = createTestShot(beanId = testBean1.id, extractionTimeSeconds = 35)
        
        shotDao.insertShot(shot1)
        shotDao.insertShot(shot2)
        shotDao.insertShot(shot3)
        
        // When - get shots with extraction time between 22 and 30 seconds
        val shotsInRange = shotDao.getShotsByExtractionTimeRange(22, 30).first()
        
        // Then
        assertEquals("Should return shots in extraction time range", 1, shotsInRange.size)
        assertEquals("Should return shot with 25 second extraction", 25, shotsInRange[0].extractionTimeSeconds)
    }
    
    /**
     * Helper function to create test shots with default values.
     */
    private fun createTestShot(
        beanId: String,
        coffeeWeightIn: Double = 18.0,
        coffeeWeightOut: Double = 36.0,
        extractionTimeSeconds: Int = 27,
        grinderSetting: String = "15",
        notes: String = "Test notes",
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