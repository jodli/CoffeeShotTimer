package com.jodli.coffeeshottimer.data.repository

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
 * Integration tests for GrinderConfigRepository.
 * Tests repository operations with real database interactions.
 */
@RunWith(RobolectricTestRunner::class)
class GrinderConfigRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: GrinderConfigRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = GrinderConfigRepository(database.grinderConfigDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun saveConfig_validConfig_succeeds() = runTest {
        // Given
        val config = createTestConfig(1, 10)

        // When
        val result = repository.saveConfig(config)

        // Then
        assertTrue("Saving valid config should succeed", result.isSuccess)

        val retrievedConfig = repository.getConfigById(config.id).getOrNull()
        assertNotNull("Config should be retrievable after saving", retrievedConfig)
        assertEquals("Config scale min should match", config.scaleMin, retrievedConfig?.scaleMin)
        assertEquals("Config scale max should match", config.scaleMax, retrievedConfig?.scaleMax)
    }

    @Test
    fun saveConfig_invalidConfig_fails() = runTest {
        // Given
        val invalidConfig = createTestConfig(10, 5) // Min > Max

        // When
        val result = repository.saveConfig(invalidConfig)

        // Then
        assertTrue("Saving invalid config should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun getCurrentConfig_noConfig_returnsNull() = runTest {
        // When
        val result = repository.getCurrentConfig()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertNull("Should return null when no config exists", result.getOrNull())
    }

    @Test
    fun getCurrentConfig_withConfigs_returnsMostRecent() = runTest {
        // Given
        val olderConfig = createTestConfig(1, 10, LocalDateTime.now().minusHours(2))
        val newerConfig = createTestConfig(30, 80, LocalDateTime.now().minusHours(1))

        repository.saveConfig(olderConfig)
        repository.saveConfig(newerConfig)

        // When
        val result = repository.getCurrentConfig()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return most recent config", newerConfig.id, result.getOrNull()?.id)
    }

    @Test
    fun getCurrentConfigFlow_emitsUpdatesReactively() = runTest {
        // Given
        val config = createTestConfig(1, 10)

        // When - Initially empty
        val initialResult = repository.getCurrentConfigFlow().first()

        // Then
        assertTrue("Should succeed initially", initialResult.isSuccess)
        assertNull("Should initially be null", initialResult.getOrNull())

        // When - Save config
        repository.saveConfig(config)
        val afterSaveResult = repository.getCurrentConfigFlow().first()

        // Then
        assertTrue("Should succeed after save", afterSaveResult.isSuccess)
        assertEquals("Should emit the saved config", config.id, afterSaveResult.getOrNull()?.id)
    }

    @Test
    fun getConfigById_existingConfig_returnsConfig() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        repository.saveConfig(config)

        // When
        val result = repository.getConfigById(config.id)

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return correct config", config.id, result.getOrNull()?.id)
    }

    @Test
    fun getConfigById_nonexistentConfig_returnsNull() = runTest {
        // When
        val result = repository.getConfigById("nonexistent-id")

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertNull("Should return null for nonexistent config", result.getOrNull())
    }

    @Test
    fun getConfigById_emptyId_fails() = runTest {
        // When
        val result = repository.getConfigById("")

        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }

    @Test
    fun getOrCreateDefaultConfig_noExistingConfig_createsDefault() = runTest {
        // When
        val result = repository.getOrCreateDefaultConfig()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        val config = result.getOrNull()
        assertNotNull("Should return a config", config)
        assertEquals("Should be default min", 1, config?.scaleMin)
        assertEquals("Should be default max", 10, config?.scaleMax)

        // Verify it was saved
        val currentConfig = repository.getCurrentConfig().getOrNull()
        assertEquals("Should be saved as current config", config?.id, currentConfig?.id)
    }

    @Test
    fun getOrCreateDefaultConfig_existingConfig_returnsExisting() = runTest {
        // Given
        val existingConfig = createTestConfig(30, 80)
        repository.saveConfig(existingConfig)

        // When
        val result = repository.getOrCreateDefaultConfig()

        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return existing config", existingConfig.id, result.getOrNull()?.id)
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
