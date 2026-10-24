package com.jodli.coffeeshottimer.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime

/**
 * Unit tests for GrinderConfigDao operations.
 * Tests all CRUD operations and queries for GrinderConfiguration entity.
 */
@RunWith(RobolectricTestRunner::class)
class GrinderConfigDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var grinderConfigDao: GrinderConfigDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        grinderConfigDao = database.grinderConfigDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertConfig_insertsSuccessfully() = runTest {
        // Given
        val config = createTestConfig(1, 10)

        // When
        grinderConfigDao.insertConfig(config)

        // Then
        val retrievedConfig = grinderConfigDao.getConfigById(config.id)
        assertNotNull("Config should be inserted", retrievedConfig)
        assertEquals("Config ID should match", config.id, retrievedConfig?.id)
        assertEquals("Scale min should match", config.scaleMin, retrievedConfig?.scaleMin)
        assertEquals("Scale max should match", config.scaleMax, retrievedConfig?.scaleMax)
    }

    @Test
    fun getCurrentConfig_returnsNullWhenEmpty() = runTest {
        // When
        val result = grinderConfigDao.getCurrentConfig()

        // Then
        assertNull("Should return null when no configs exist", result)
    }

    @Test
    fun getCurrentConfig_returnsMostRecentConfig() = runTest {
        // Given
        val olderConfig = createTestConfig(1, 10, LocalDateTime.now().minusHours(2))
        val newerConfig = createTestConfig(30, 80, LocalDateTime.now().minusHours(1))

        grinderConfigDao.insertConfig(olderConfig)
        grinderConfigDao.insertConfig(newerConfig)

        // When
        val result = grinderConfigDao.getCurrentConfig()

        // Then
        assertNotNull("Should return a config", result)
        assertEquals("Should return the newer config", newerConfig.id, result?.id)
    }

    @Test
    fun getCurrentConfigFlow_emitsUpdatesReactively() = runTest {
        // Given
        val config = createTestConfig(1, 10)

        // When - Initially empty
        val initialResult = grinderConfigDao.getCurrentConfigFlow().first()

        // Then
        assertNull("Should initially be null", initialResult)

        // When - Insert config
        grinderConfigDao.insertConfig(config)
        val afterInsertResult = grinderConfigDao.getCurrentConfigFlow().first()

        // Then
        assertNotNull("Should emit the inserted config", afterInsertResult)
        assertEquals("Should emit the correct config", config.id, afterInsertResult?.id)
    }

    @Test
    fun getAllConfigs_returnsEmptyListWhenEmpty() = runTest {
        // When
        val result = grinderConfigDao.getAllConfigs().first()

        // Then
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun getAllConfigs_returnsConfigsOrderedByCreationDate() = runTest {
        // Given
        val config1 = createTestConfig(1, 10, LocalDateTime.now().minusHours(3))
        val config2 = createTestConfig(30, 80, LocalDateTime.now().minusHours(2))
        val config3 = createTestConfig(50, 60, LocalDateTime.now().minusHours(1))

        grinderConfigDao.insertConfig(config1)
        grinderConfigDao.insertConfig(config2)
        grinderConfigDao.insertConfig(config3)

        // When
        val result = grinderConfigDao.getAllConfigs().first()

        // Then
        assertEquals("Should return all configs", 3, result.size)
        assertEquals("First should be newest", config3.id, result[0].id)
        assertEquals("Second should be middle", config2.id, result[1].id)
        assertEquals("Third should be oldest", config1.id, result[2].id)
    }

    @Test
    fun getConfigById_returnsCorrectConfig() = runTest {
        // Given
        val config1 = createTestConfig(1, 10)
        val config2 = createTestConfig(30, 80)

        grinderConfigDao.insertConfig(config1)
        grinderConfigDao.insertConfig(config2)

        // When
        val result = grinderConfigDao.getConfigById(config2.id)

        // Then
        assertNotNull("Should return the config", result)
        assertEquals("Should return correct config", config2.id, result?.id)
        assertEquals("Scale min should match", config2.scaleMin, result?.scaleMin)
        assertEquals("Scale max should match", config2.scaleMax, result?.scaleMax)
    }

    @Test
    fun getConfigById_returnsNullForNonexistentId() = runTest {
        // When
        val result = grinderConfigDao.getConfigById("nonexistent-id")

        // Then
        assertNull("Should return null for nonexistent ID", result)
    }

    @Test
    fun updateConfig_updatesSuccessfully() = runTest {
        // Given
        val originalConfig = createTestConfig(1, 10)
        grinderConfigDao.insertConfig(originalConfig)

        val updatedConfig = originalConfig.copy(scaleMin = 5, scaleMax = 15)

        // When
        grinderConfigDao.updateConfig(updatedConfig)

        // Then
        val result = grinderConfigDao.getConfigById(originalConfig.id)
        assertNotNull("Config should still exist", result)
        assertEquals("Scale min should be updated", 5, result?.scaleMin)
        assertEquals("Scale max should be updated", 15, result?.scaleMax)
        assertEquals("ID should remain the same", originalConfig.id, result?.id)
    }

    @Test
    fun deleteConfig_deletesSuccessfully() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        grinderConfigDao.insertConfig(config)

        // Verify it exists
        assertNotNull("Config should exist before deletion", grinderConfigDao.getConfigById(config.id))

        // When
        grinderConfigDao.deleteConfig(config)

        // Then
        assertNull("Config should be deleted", grinderConfigDao.getConfigById(config.id))
    }

    @Test
    fun deleteAllConfigs_deletesAllConfigs() = runTest {
        // Given
        val config1 = createTestConfig(1, 10)
        val config2 = createTestConfig(30, 80)
        val config3 = createTestConfig(50, 60)

        grinderConfigDao.insertConfig(config1)
        grinderConfigDao.insertConfig(config2)
        grinderConfigDao.insertConfig(config3)

        // Verify they exist
        assertEquals("Should have 3 configs", 3, grinderConfigDao.getAllConfigs().first().size)

        // When
        grinderConfigDao.deleteAllConfigs()

        // Then
        assertTrue("Should have no configs", grinderConfigDao.getAllConfigs().first().isEmpty())
        assertEquals("Count should be 0", 0, grinderConfigDao.getConfigCount())
    }

    @Test
    fun getConfigCount_returnsCorrectCount() = runTest {
        // Given - Initially empty
        assertEquals("Should start with 0 configs", 0, grinderConfigDao.getConfigCount())

        // When - Add configs
        grinderConfigDao.insertConfig(createTestConfig(1, 10))
        grinderConfigDao.insertConfig(createTestConfig(30, 80))

        // Then
        assertEquals("Should have 2 configs", 2, grinderConfigDao.getConfigCount())
    }

    @Test
    fun getConfigByRange_returnsMatchingConfig() = runTest {
        // Given
        val config1 = createTestConfig(1, 10)
        val config2 = createTestConfig(30, 80)

        grinderConfigDao.insertConfig(config1)
        grinderConfigDao.insertConfig(config2)

        // When
        val result = grinderConfigDao.getConfigByRange(30, 80)

        // Then
        assertNotNull("Should find matching config", result)
        assertEquals("Should return correct config", config2.id, result?.id)
    }

    @Test
    fun getConfigByRange_returnsNullForNonexistentRange() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        grinderConfigDao.insertConfig(config)

        // When
        val result = grinderConfigDao.getConfigByRange(50, 60)

        // Then
        assertNull("Should return null for nonexistent range", result)
    }

    @Test
    fun insertConfig_withReplaceStrategy_replacesExistingConfig() = runTest {
        // Given
        val originalConfig = createTestConfig(1, 10)
        grinderConfigDao.insertConfig(originalConfig)

        val replacementConfig = originalConfig.copy(scaleMin = 5, scaleMax = 15)

        // When - Insert with same ID (should replace due to REPLACE strategy)
        grinderConfigDao.insertConfig(replacementConfig)

        // Then
        assertEquals("Should still have only 1 config", 1, grinderConfigDao.getConfigCount())

        val result = grinderConfigDao.getConfigById(originalConfig.id)
        assertNotNull("Config should exist", result)
        assertEquals("Scale min should be updated", 5, result?.scaleMin)
        assertEquals("Scale max should be updated", 15, result?.scaleMax)
    }

    private fun createTestConfig(
        scaleMin: Int,
        scaleMax: Int,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): GrinderConfiguration {
        return GrinderConfiguration(
            scaleMin = scaleMin,
            scaleMax = scaleMax,
            createdAt = createdAt
        )
    }
}
