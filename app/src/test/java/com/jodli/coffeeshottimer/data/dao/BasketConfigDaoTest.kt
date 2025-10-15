package com.jodli.coffeeshottimer.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jodli.coffeeshottimer.data.database.AppDatabase
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for BasketConfigDao operations.
 * Tests all CRUD operations and queries for BasketConfiguration entity.
 */
@RunWith(RobolectricTestRunner::class)
class BasketConfigDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var basketConfigDao: BasketConfigDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        basketConfigDao = database.basketConfigDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun createTestConfig(
        coffeeInMin: Float = 14f,
        coffeeInMax: Float = 22f,
        coffeeOutMin: Float = 28f,
        coffeeOutMax: Float = 55f,
        createdAt: LocalDateTime = LocalDateTime.now(),
        isActive: Boolean = true
    ): BasketConfiguration {
        return BasketConfiguration(
            id = UUID.randomUUID().toString(),
            coffeeInMin = coffeeInMin,
            coffeeInMax = coffeeInMax,
            coffeeOutMin = coffeeOutMin,
            coffeeOutMax = coffeeOutMax,
            createdAt = createdAt,
            isActive = isActive
        )
    }

    @Test
    fun insertConfig_insertsSuccessfully() = runTest {
        // Given
        val config = createTestConfig()

        // When
        basketConfigDao.insertConfig(config)

        // Then
        val retrievedConfig = basketConfigDao.getConfigById(config.id)
        assertNotNull("Config should be inserted", retrievedConfig)
        assertEquals("Config ID should match", config.id, retrievedConfig?.id)
        assertEquals("Coffee in min should match", config.coffeeInMin, retrievedConfig?.coffeeInMin)
        assertEquals("Coffee in max should match", config.coffeeInMax, retrievedConfig?.coffeeInMax)
        assertEquals("Coffee out min should match", config.coffeeOutMin, retrievedConfig?.coffeeOutMin)
        assertEquals("Coffee out max should match", config.coffeeOutMax, retrievedConfig?.coffeeOutMax)
        assertEquals("isActive should match", config.isActive, retrievedConfig?.isActive)
    }

    @Test
    fun getActiveConfig_returnsNullWhenEmpty() = runTest {
        // When
        val result = basketConfigDao.getActiveConfig()

        // Then
        assertNull("Should return null when no configs exist", result)
    }

    @Test
    fun getActiveConfig_returnsOnlyActiveConfig() = runTest {
        // Given
        val inactiveConfig = createTestConfig(isActive = false, createdAt = LocalDateTime.now().minusHours(2))
        val activeConfig = createTestConfig(isActive = true, createdAt = LocalDateTime.now().minusHours(1))
        val anotherInactiveConfig = createTestConfig(isActive = false, createdAt = LocalDateTime.now())

        basketConfigDao.insertConfig(inactiveConfig)
        basketConfigDao.insertConfig(activeConfig)
        basketConfigDao.insertConfig(anotherInactiveConfig)

        // When
        val result = basketConfigDao.getActiveConfig()

        // Then
        assertNotNull("Should return a config", result)
        assertEquals("Should return the active config", activeConfig.id, result?.id)
        assertTrue("Returned config should be active", result?.isActive ?: false)
    }

    @Test
    fun getActiveConfig_returnsMostRecentWhenMultipleActive() = runTest {
        // Given - multiple active configs (edge case)
        val olderActive = createTestConfig(isActive = true, createdAt = LocalDateTime.now().minusHours(2))
        val newerActive = createTestConfig(isActive = true, createdAt = LocalDateTime.now().minusHours(1))

        basketConfigDao.insertConfig(olderActive)
        basketConfigDao.insertConfig(newerActive)

        // When
        val result = basketConfigDao.getActiveConfig()

        // Then
        assertNotNull("Should return a config", result)
        assertEquals("Should return the newer active config", newerActive.id, result?.id)
    }

    @Test
    fun getActiveConfigFlow_emitsUpdatesReactively() = runTest {
        // Given
        val config = createTestConfig(isActive = true)

        // When - Initially empty
        val initialResult = basketConfigDao.getActiveConfigFlow().first()

        // Then
        assertNull("Should initially be null", initialResult)

        // When - Insert active config
        basketConfigDao.insertConfig(config)
        val afterInsertResult = basketConfigDao.getActiveConfigFlow().first()

        // Then
        assertNotNull("Should emit the inserted config", afterInsertResult)
        assertEquals("Should emit the correct config", config.id, afterInsertResult?.id)

        // When - Deactivate the config
        basketConfigDao.deactivateAllConfigs()
        val afterDeactivateResult = basketConfigDao.getActiveConfigFlow().first()

        // Then
        assertNull("Should emit null after deactivation", afterDeactivateResult)
    }

    @Test
    fun getAllConfigs_returnsEmptyListWhenEmpty() = runTest {
        // When
        val result = basketConfigDao.getAllConfigs().first()

        // Then
        assertTrue("Should return empty list", result.isEmpty())
    }

    @Test
    fun getAllConfigs_returnsConfigsOrderedByCreationDate() = runTest {
        // Given
        val config1 = createTestConfig(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            createdAt = LocalDateTime.now().minusHours(3)
        )
        val config2 = createTestConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            createdAt = LocalDateTime.now().minusHours(2)
        )
        val config3 = createTestConfig(
            coffeeInMin = 18f,
            coffeeInMax = 25f,
            createdAt = LocalDateTime.now().minusHours(1)
        )

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)
        basketConfigDao.insertConfig(config3)

        // When
        val result = basketConfigDao.getAllConfigs().first()

        // Then
        assertEquals("Should return all configs", 3, result.size)
        assertEquals("First should be newest", config3.id, result[0].id)
        assertEquals("Second should be middle", config2.id, result[1].id)
        assertEquals("Third should be oldest", config1.id, result[2].id)
    }

    @Test
    fun getConfigById_returnsCorrectConfig() = runTest {
        // Given
        val config1 = createTestConfig(coffeeInMin = 7f, coffeeInMax = 12f)
        val config2 = createTestConfig(coffeeInMin = 14f, coffeeInMax = 22f)

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)

        // When
        val result = basketConfigDao.getConfigById(config2.id)

        // Then
        assertNotNull("Should return the config", result)
        assertEquals("Should return correct config", config2.id, result?.id)
        assertEquals("Coffee in min should match", config2.coffeeInMin, result?.coffeeInMin)
        assertEquals("Coffee in max should match", config2.coffeeInMax, result?.coffeeInMax)
    }

    @Test
    fun getConfigById_returnsNullForNonexistentId() = runTest {
        // When
        val result = basketConfigDao.getConfigById("nonexistent-id")

        // Then
        assertNull("Should return null for nonexistent ID", result)
    }

    @Test
    fun updateConfig_updatesSuccessfully() = runTest {
        // Given
        val originalConfig = createTestConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
        basketConfigDao.insertConfig(originalConfig)

        val updatedConfig = originalConfig.copy(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            coffeeOutMin = 20f,
            coffeeOutMax = 40f
        )

        // When
        basketConfigDao.updateConfig(updatedConfig)

        // Then
        val result = basketConfigDao.getConfigById(originalConfig.id)
        assertNotNull("Config should still exist", result)
        assertEquals("Coffee in min should be updated", 7f, result?.coffeeInMin)
        assertEquals("Coffee in max should be updated", 12f, result?.coffeeInMax)
        assertEquals("Coffee out min should be updated", 20f, result?.coffeeOutMin)
        assertEquals("Coffee out max should be updated", 40f, result?.coffeeOutMax)
        assertEquals("ID should remain the same", originalConfig.id, result?.id)
    }

    @Test
    fun deleteConfig_deletesSuccessfully() = runTest {
        // Given
        val config = createTestConfig()
        basketConfigDao.insertConfig(config)

        // Verify it exists
        assertNotNull("Config should exist before deletion", basketConfigDao.getConfigById(config.id))

        // When
        basketConfigDao.deleteConfig(config)

        // Then
        assertNull("Config should be deleted", basketConfigDao.getConfigById(config.id))
    }

    @Test
    fun deleteAllConfigs_deletesAllConfigs() = runTest {
        // Given
        val config1 = createTestConfig(coffeeInMin = 7f, coffeeInMax = 12f)
        val config2 = createTestConfig(coffeeInMin = 14f, coffeeInMax = 22f)
        val config3 = createTestConfig(coffeeInMin = 18f, coffeeInMax = 25f)

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)
        basketConfigDao.insertConfig(config3)

        // Verify they exist
        assertEquals("Should have 3 configs", 3, basketConfigDao.getConfigCount())

        // When
        basketConfigDao.deleteAllConfigs()

        // Then
        assertEquals("Should have 0 configs", 0, basketConfigDao.getConfigCount())
        assertNull("Config 1 should be deleted", basketConfigDao.getConfigById(config1.id))
        assertNull("Config 2 should be deleted", basketConfigDao.getConfigById(config2.id))
        assertNull("Config 3 should be deleted", basketConfigDao.getConfigById(config3.id))
    }

    @Test
    fun getConfigCount_returnsCorrectCount() = runTest {
        // Initially
        assertEquals("Should initially be 0", 0, basketConfigDao.getConfigCount())

        // Add one config
        basketConfigDao.insertConfig(createTestConfig())
        assertEquals("Should be 1 after first insert", 1, basketConfigDao.getConfigCount())

        // Add more configs
        basketConfigDao.insertConfig(createTestConfig(coffeeInMin = 7f, coffeeInMax = 12f))
        basketConfigDao.insertConfig(createTestConfig(coffeeInMin = 18f, coffeeInMax = 25f))
        assertEquals("Should be 3 after all inserts", 3, basketConfigDao.getConfigCount())
    }

    @Test
    fun deactivateAllConfigs_deactivatesAllConfigs() = runTest {
        // Given
        val config1 = createTestConfig(isActive = true)
        val config2 = createTestConfig(isActive = true)
        val config3 = createTestConfig(isActive = true)

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)
        basketConfigDao.insertConfig(config3)

        // When
        basketConfigDao.deactivateAllConfigs()

        // Then
        val allConfigs = basketConfigDao.getAllConfigs().first()
        allConfigs.forEach { config ->
            assertFalse("Config ${config.id} should be inactive", config.isActive)
        }
        assertNull("No active config should exist", basketConfigDao.getActiveConfig())
    }

    @Test
    fun activateConfig_activatesSpecificConfig() = runTest {
        // Given
        val config1 = createTestConfig(isActive = false)
        val config2 = createTestConfig(isActive = false)

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)

        // When
        basketConfigDao.activateConfig(config2.id)

        // Then
        val result1 = basketConfigDao.getConfigById(config1.id)
        val result2 = basketConfigDao.getConfigById(config2.id)

        assertFalse("Config 1 should remain inactive", result1?.isActive ?: true)
        assertTrue("Config 2 should be active", result2?.isActive ?: false)

        val activeConfig = basketConfigDao.getActiveConfig()
        assertEquals("Active config should be config2", config2.id, activeConfig?.id)
    }

    @Test
    fun getConfigByRanges_findsMatchingConfig() = runTest {
        // Given
        val config1 = createTestConfig(
            coffeeInMin = 7f,
            coffeeInMax = 12f,
            coffeeOutMin = 20f,
            coffeeOutMax = 40f
        )
        val config2 = createTestConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)

        // When
        val result = basketConfigDao.getConfigByRanges(14f, 22f, 28f, 55f)

        // Then
        assertNotNull("Should find matching config", result)
        assertEquals("Should return correct config", config2.id, result?.id)
    }

    @Test
    fun getConfigByRanges_returnsNullForNonMatchingRanges() = runTest {
        // Given
        val config = createTestConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f,
            coffeeOutMin = 28f,
            coffeeOutMax = 55f
        )
        basketConfigDao.insertConfig(config)

        // When
        val result = basketConfigDao.getConfigByRanges(10f, 20f, 25f, 50f)

        // Then
        assertNull("Should return null for non-matching ranges", result)
    }

    @Test
    fun deleteOldConfigs_keepsOnlyRecentConfigs() = runTest {
        // Given - Insert 5 configs with different creation times
        val configs = (1..5).map { index ->
            createTestConfig(
                coffeeInMin = (6 + index).toFloat(),
                coffeeInMax = (12 + index).toFloat(),
                createdAt = LocalDateTime.now().minusHours((6 - index).toLong())
            )
        }

        configs.forEach { basketConfigDao.insertConfig(it) }

        // When - Keep only 3 most recent
        basketConfigDao.deleteOldConfigs(keepCount = 3)

        // Then
        val remainingConfigs = basketConfigDao.getAllConfigs().first()
        assertEquals("Should have 3 configs remaining", 3, remainingConfigs.size)

        // The 3 most recent configs (indices 2, 3, 4) should remain
        assertTrue("Config 3 should remain", remainingConfigs.any { it.id == configs[2].id })
        assertTrue("Config 4 should remain", remainingConfigs.any { it.id == configs[3].id })
        assertTrue("Config 5 should remain", remainingConfigs.any { it.id == configs[4].id })

        // The 2 oldest configs should be deleted
        assertFalse("Config 1 should be deleted", remainingConfigs.any { it.id == configs[0].id })
        assertFalse("Config 2 should be deleted", remainingConfigs.any { it.id == configs[1].id })
    }

    @Test
    fun deleteOldConfigs_handlesFewerConfigsThanKeepCount() = runTest {
        // Given - Only 2 configs
        val config1 = createTestConfig()
        val config2 = createTestConfig(coffeeInMin = 7f, coffeeInMax = 12f)

        basketConfigDao.insertConfig(config1)
        basketConfigDao.insertConfig(config2)

        // When - Try to keep 5 (more than exist)
        basketConfigDao.deleteOldConfigs(keepCount = 5)

        // Then - All configs should remain
        assertEquals("Should still have 2 configs", 2, basketConfigDao.getConfigCount())
    }

    @Test
    fun insertConfig_replacesOnConflict() = runTest {
        // Given
        val config = createTestConfig(
            coffeeInMin = 14f,
            coffeeInMax = 22f
        )
        basketConfigDao.insertConfig(config)

        // When - Insert same ID with different values
        val updatedConfig = config.copy(
            coffeeInMin = 7f,
            coffeeInMax = 12f
        )
        basketConfigDao.insertConfig(updatedConfig)

        // Then
        val result = basketConfigDao.getConfigById(config.id)
        assertEquals("Should have replaced values", 7f, result?.coffeeInMin)
        assertEquals("Should have replaced values", 12f, result?.coffeeInMax)
        assertEquals("Should still be only one config", 1, basketConfigDao.getConfigCount())
    }
}
