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
    fun saveConfig_duplicateRange_fails() = runTest {
        // Given
        val config1 = createTestConfig(1, 10)
        val config2 = createTestConfig(1, 10) // Same range, different ID
        
        repository.saveConfig(config1)
        
        // When
        val result = repository.saveConfig(config2)
        
        // Then
        assertTrue("Saving duplicate range should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
        assertTrue("Error should mention duplicate range", 
            result.exceptionOrNull()?.message?.contains("already exists") == true)
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
    fun getAllConfigs_emptyDatabase_returnsEmptyList() = runTest {
        // When
        val result = repository.getAllConfigs().first()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertTrue("Should return empty list", result.getOrNull()?.isEmpty() == true)
    }
    
    @Test
    fun getAllConfigs_withConfigs_returnsOrderedList() = runTest {
        // Given
        val config1 = createTestConfig(1, 10, LocalDateTime.now().minusHours(3))
        val config2 = createTestConfig(30, 80, LocalDateTime.now().minusHours(2))
        val config3 = createTestConfig(50, 60, LocalDateTime.now().minusHours(1))
        
        repository.saveConfig(config1)
        repository.saveConfig(config2)
        repository.saveConfig(config3)
        
        // When
        val result = repository.getAllConfigs().first()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        val configs = result.getOrNull()
        assertNotNull("Should return configs", configs)
        assertEquals("Should return all configs", 3, configs?.size)
        assertEquals("First should be newest", config3.id, configs?.get(0)?.id)
        assertEquals("Last should be oldest", config1.id, configs?.get(2)?.id)
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
    fun updateConfig_validUpdate_succeeds() = runTest {
        // Given
        val originalConfig = createTestConfig(1, 10)
        repository.saveConfig(originalConfig)
        
        val updatedConfig = originalConfig.copy(scaleMin = 5, scaleMax = 15)
        
        // When
        val result = repository.updateConfig(updatedConfig)
        
        // Then
        assertTrue("Update should succeed", result.isSuccess)
        
        val retrievedConfig = repository.getConfigById(originalConfig.id).getOrNull()
        assertEquals("Scale min should be updated", 5, retrievedConfig?.scaleMin)
        assertEquals("Scale max should be updated", 15, retrievedConfig?.scaleMax)
    }
    
    @Test
    fun updateConfig_nonexistentConfig_fails() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        
        // When
        val result = repository.updateConfig(config)
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }
    
    @Test
    fun deleteConfig_existingConfig_succeeds() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        repository.saveConfig(config)
        
        // When
        val result = repository.deleteConfig(config)
        
        // Then
        assertTrue("Delete should succeed", result.isSuccess)
        
        val retrievedConfig = repository.getConfigById(config.id).getOrNull()
        assertNull("Config should be deleted", retrievedConfig)
    }
    
    @Test
    fun deleteConfig_nonexistentConfig_fails() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        
        // When
        val result = repository.deleteConfig(config)
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be not found error", result.exceptionOrNull() is RepositoryException.NotFoundError)
    }
    
    @Test
    fun deleteAllConfigs_removesAllConfigs() = runTest {
        // Given
        repository.saveConfig(createTestConfig(1, 10))
        repository.saveConfig(createTestConfig(30, 80))
        repository.saveConfig(createTestConfig(50, 60))
        
        // When
        val result = repository.deleteAllConfigs()
        
        // Then
        assertTrue("Delete all should succeed", result.isSuccess)
        
        val countResult = repository.getConfigCount()
        assertTrue("Count should succeed", countResult.isSuccess)
        assertEquals("Should have no configs", 0, countResult.getOrNull())
    }
    
    @Test
    fun getConfigCount_returnsCorrectCount() = runTest {
        // Given - Initially empty
        val initialCountResult = repository.getConfigCount()
        assertTrue("Should succeed", initialCountResult.isSuccess)
        assertEquals("Should start with 0", 0, initialCountResult.getOrNull())
        
        // When - Add configs
        repository.saveConfig(createTestConfig(1, 10))
        repository.saveConfig(createTestConfig(30, 80))
        
        val finalCountResult = repository.getConfigCount()
        
        // Then
        assertTrue("Should succeed", finalCountResult.isSuccess)
        assertEquals("Should have 2 configs", 2, finalCountResult.getOrNull())
    }
    
    @Test
    fun getConfigByRange_existingRange_returnsConfig() = runTest {
        // Given
        val config = createTestConfig(30, 80)
        repository.saveConfig(config)
        
        // When
        val result = repository.getConfigByRange(30, 80)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertEquals("Should return matching config", config.id, result.getOrNull()?.id)
    }
    
    @Test
    fun getConfigByRange_nonexistentRange_returnsNull() = runTest {
        // Given
        repository.saveConfig(createTestConfig(1, 10))
        
        // When
        val result = repository.getConfigByRange(30, 80)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertNull("Should return null for nonexistent range", result.getOrNull())
    }
    
    @Test
    fun getConfigByRange_invalidRange_fails() = runTest {
        // When
        val result = repository.getConfigByRange(10, 5) // Min > Max
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun validateConfig_validConfig_returnsValid() = runTest {
        // Given
        val config = createTestConfig(1, 10)
        
        // When
        val result = repository.validateConfig(config)
        
        // Then
        assertTrue("Should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }
    
    @Test
    fun validateConfig_invalidConfig_returnsInvalid() = runTest {
        // Given
        val config = createTestConfig(10, 5) // Min > Max
        
        // When
        val result = repository.validateConfig(config)
        
        // Then
        assertFalse("Should be invalid", result.isValid)
        assertFalse("Should have errors", result.errors.isEmpty())
    }
    
    @Test
    fun validateConfig_duplicateRange_returnsInvalid() = runTest {
        // Given
        val config1 = createTestConfig(1, 10)
        val config2 = createTestConfig(1, 10) // Same range, different ID
        
        repository.saveConfig(config1)
        
        // When
        val result = repository.validateConfig(config2)
        
        // Then
        assertFalse("Should be invalid", result.isValid)
        assertTrue("Should contain duplicate error", 
            result.errors.any { it.contains("already exists") })
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
    
    @Test
    fun savePresetConfig_validIndex_succeeds() = runTest {
        // When
        val result = repository.savePresetConfig(0) // First preset
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        val currentConfig = repository.getCurrentConfig().getOrNull()
        assertNotNull("Should have current config", currentConfig)
        
        val firstPreset = GrinderConfiguration.COMMON_PRESETS[0]
        assertEquals("Should match preset min", firstPreset.scaleMin, currentConfig?.scaleMin)
        assertEquals("Should match preset max", firstPreset.scaleMax, currentConfig?.scaleMax)
    }
    
    @Test
    fun savePresetConfig_invalidIndex_fails() = runTest {
        // When
        val result = repository.savePresetConfig(-1)
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun cleanupOldConfigs_removesOldConfigs() = runTest {
        // Given - Add 5 configs
        repeat(5) { index ->
            val config = createTestConfig(
                scaleMin = index * 10 + 1,
                scaleMax = index * 10 + 10,
                createdAt = LocalDateTime.now().minusHours((5 - index).toLong())
            )
            repository.saveConfig(config)
        }
        
        // When - Keep only 3
        val result = repository.cleanupOldConfigs(3)
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        
        val countResult = repository.getConfigCount()
        assertTrue("Count should succeed", countResult.isSuccess)
        assertEquals("Should have 3 configs remaining", 3, countResult.getOrNull())
    }
    
    @Test
    fun cleanupOldConfigs_invalidKeepCount_fails() = runTest {
        // When
        val result = repository.cleanupOldConfigs(0)
        
        // Then
        assertTrue("Should fail", result.isFailure)
        assertTrue("Should be validation error", result.exceptionOrNull() is RepositoryException.ValidationError)
    }
    
    @Test
    fun hasAnyConfig_noConfigs_returnsFalse() = runTest {
        // When
        val result = repository.hasAnyConfig()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertFalse("Should return false when no configs exist", result.getOrNull() == true)
    }
    
    @Test
    fun hasAnyConfig_withConfigs_returnsTrue() = runTest {
        // Given
        repository.saveConfig(createTestConfig(1, 10))
        
        // When
        val result = repository.hasAnyConfig()
        
        // Then
        assertTrue("Should succeed", result.isSuccess)
        assertTrue("Should return true when configs exist", result.getOrNull() == true)
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