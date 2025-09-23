package com.jodli.coffeeshottimer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jodli.coffeeshottimer.data.model.BasketConfiguration
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for BasketConfiguration entity operations.
 * Provides methods for CRUD operations and queries related to basket configuration.
 */
@Dao
interface BasketConfigDao {

    /**
     * Get the current active basket configuration.
     * Returns the most recent active configuration.
     */
    @Query("SELECT * FROM basket_configuration WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getActiveConfig(): BasketConfiguration?

    /**
     * Get the current active basket configuration as a Flow for reactive updates.
     */
    @Query("SELECT * FROM basket_configuration WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun getActiveConfigFlow(): Flow<BasketConfiguration?>

    /**
     * Get all basket configurations ordered by creation date (newest first).
     * Useful for configuration history or allowing users to switch between configurations.
     */
    @Query("SELECT * FROM basket_configuration ORDER BY createdAt DESC")
    fun getAllConfigs(): Flow<List<BasketConfiguration>>

    /**
     * Get a specific basket configuration by ID.
     */
    @Query("SELECT * FROM basket_configuration WHERE id = :configId")
    suspend fun getConfigById(configId: String): BasketConfiguration?

    /**
     * Insert a new basket configuration.
     * Uses REPLACE strategy to handle any conflicts.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: BasketConfiguration)

    /**
     * Update an existing basket configuration.
     */
    @Update
    suspend fun updateConfig(config: BasketConfiguration)

    /**
     * Delete a basket configuration.
     */
    @Delete
    suspend fun deleteConfig(config: BasketConfiguration)

    /**
     * Delete all basket configurations.
     * Useful for reset functionality.
     */
    @Query("DELETE FROM basket_configuration")
    suspend fun deleteAllConfigs()

    /**
     * Get count of basket configurations.
     */
    @Query("SELECT COUNT(*) FROM basket_configuration")
    suspend fun getConfigCount(): Int

    /**
     * Deactivate all basket configurations.
     * Used before setting a new active configuration.
     */
    @Query("UPDATE basket_configuration SET isActive = 0")
    suspend fun deactivateAllConfigs()

    /**
     * Set a specific configuration as active.
     * @param configId The ID of the configuration to activate
     */
    @Query("UPDATE basket_configuration SET isActive = 1 WHERE id = :configId")
    suspend fun activateConfig(configId: String)

    /**
     * Check if a configuration with the same weight ranges already exists.
     * Useful for preventing duplicate configurations.
     */
    @Query(
        """
        SELECT * FROM basket_configuration 
        WHERE coffeeInMin = :coffeeInMin 
        AND coffeeInMax = :coffeeInMax 
        AND coffeeOutMin = :coffeeOutMin 
        AND coffeeOutMax = :coffeeOutMax 
        LIMIT 1
    """
    )
    suspend fun getConfigByRanges(
        coffeeInMin: Float,
        coffeeInMax: Float,
        coffeeOutMin: Float,
        coffeeOutMax: Float
    ): BasketConfiguration?

    /**
     * Delete old configurations, keeping only the most recent N configurations.
     * Useful for cleanup to prevent unlimited configuration history.
     */
    @Query(
        """
        DELETE FROM basket_configuration 
        WHERE id NOT IN (
            SELECT id FROM basket_configuration 
            ORDER BY createdAt DESC 
            LIMIT :keepCount
        )
    """
    )
    suspend fun deleteOldConfigs(keepCount: Int = 10)
}
