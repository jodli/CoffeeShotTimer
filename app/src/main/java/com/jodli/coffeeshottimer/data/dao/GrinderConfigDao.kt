package com.jodli.coffeeshottimer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jodli.coffeeshottimer.data.model.GrinderConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for GrinderConfiguration entity operations.
 * Provides methods for CRUD operations and queries related to grinder configuration.
 */
@Dao
interface GrinderConfigDao {

    /**
     * Get the current grinder configuration (most recent).
     * Since users typically have one active configuration, this returns the latest one.
     */
    @Query("SELECT * FROM grinder_configuration ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCurrentConfig(): GrinderConfiguration?

    /**
     * Get the current grinder configuration as a Flow for reactive updates.
     */
    @Query("SELECT * FROM grinder_configuration ORDER BY createdAt DESC LIMIT 1")
    fun getCurrentConfigFlow(): Flow<GrinderConfiguration?>

    /**
     * Get all grinder configurations ordered by creation date (newest first).
     * Useful for configuration history or allowing users to switch between configurations.
     */
    @Query("SELECT * FROM grinder_configuration ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<GrinderConfiguration>>

    /**
     * Get a specific grinder configuration by ID.
     */
    @Query("SELECT * FROM grinder_configuration WHERE id = :configId")
    suspend fun getConfigById(configId: String): GrinderConfiguration?

    /**
     * Insert a new grinder configuration.
     * Uses REPLACE strategy to handle any conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: GrinderConfiguration)

    /**
     * Update an existing grinder configuration.
     */
    @Update
    suspend fun updateConfig(config: GrinderConfiguration)

    /**
     * Delete a grinder configuration.
     */
    @Delete
    suspend fun deleteConfig(config: GrinderConfiguration)

    /**
     * Delete all grinder configurations.
     * Useful for reset functionality.
     */
    @Query("DELETE FROM grinder_configuration")
    suspend fun deleteAllConfigs()

    /**
     * Get count of grinder configurations.
     */
    @Query("SELECT COUNT(*) FROM grinder_configuration")
    suspend fun getConfigCount(): Int

    /**
     * Check if a configuration with the same scale range already exists.
     * Useful for preventing duplicate configurations.
     */
    @Query("SELECT * FROM grinder_configuration WHERE scaleMin = :scaleMin AND scaleMax = :scaleMax LIMIT 1")
    suspend fun getConfigByRange(scaleMin: Int, scaleMax: Int): GrinderConfiguration?

    /**
     * Delete old configurations, keeping only the most recent N configurations.
     * Useful for cleanup to prevent unlimited configuration history.
     */
    @Query(
        """
        DELETE FROM grinder_configuration 
        WHERE id NOT IN (
            SELECT id FROM grinder_configuration 
            ORDER BY createdAt DESC 
            LIMIT :keepCount
        )
    """
    )
    suspend fun deleteOldConfigs(keepCount: Int = 5)
}
