package com.example.coffeeshottimer.data.dao

import androidx.room.*
import com.example.coffeeshottimer.data.model.Shot
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for Shot entity operations.
 * Provides methods for CRUD operations and queries related to espresso shots.
 */
@Dao
interface ShotDao {
    
    /**
     * Get all shots ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM shots ORDER BY timestamp DESC")
    fun getAllShots(): Flow<List<Shot>>
    
    /**
     * Get shots for a specific bean ordered by timestamp (newest first).
     */
    @Query("SELECT * FROM shots WHERE beanId = :beanId ORDER BY timestamp DESC")
    fun getShotsByBean(beanId: String): Flow<List<Shot>>
    
    /**
     * Get a specific shot by ID.
     */
    @Query("SELECT * FROM shots WHERE id = :shotId")
    suspend fun getShotById(shotId: String): Shot?
    
    /**
     * Get recent shots (last N shots).
     */
    @Query("SELECT * FROM shots ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentShots(limit: Int): Flow<List<Shot>>
    
    /**
     * Get shots within a date range.
     */
    @Query("SELECT * FROM shots WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getShotsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Shot>>
    
    /**
     * Get shots filtered by bean and date range.
     */
    @Query("""
        SELECT * FROM shots 
        WHERE (:beanId = '' OR beanId = :beanId)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
        ORDER BY timestamp DESC
    """)
    fun getFilteredShots(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<Shot>>
    
    /**
     * Get shots by grinder setting.
     */
    @Query("SELECT * FROM shots WHERE grinderSetting = :grinderSetting ORDER BY timestamp DESC")
    fun getShotsByGrinderSetting(grinderSetting: String): Flow<List<Shot>>
    
    /**
     * Insert a new shot.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: Shot)
    
    /**
     * Update an existing shot.
     */
    @Update
    suspend fun updateShot(shot: Shot)
    
    /**
     * Delete a shot.
     */
    @Delete
    suspend fun deleteShot(shot: Shot)
    
    /**
     * Delete all shots for a specific bean.
     */
    @Query("DELETE FROM shots WHERE beanId = :beanId")
    suspend fun deleteShotsByBean(beanId: String)
    
    /**
     * Get shot statistics for a specific bean.
     */
    @Query("""
        SELECT 
            COUNT(*) as totalShots,
            AVG(coffeeWeightIn) as avgWeightIn,
            AVG(coffeeWeightOut) as avgWeightOut,
            AVG(extractionTimeSeconds) as avgExtractionTime,
            AVG(coffeeWeightOut / coffeeWeightIn) as avgBrewRatio
        FROM shots 
        WHERE beanId = :beanId
    """)
    suspend fun getShotStatistics(beanId: String): ShotStatistics?
    
    /**
     * Get the last shot for a specific bean (for grinder setting memory).
     */
    @Query("SELECT * FROM shots WHERE beanId = :beanId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastShotForBean(beanId: String): Shot?
    
    /**
     * Get total shot count.
     */
    @Query("SELECT COUNT(*) FROM shots")
    suspend fun getTotalShotCount(): Int
    
    /**
     * Get shots with brew ratio in a specific range.
     */
    @Query("""
        SELECT * FROM shots 
        WHERE (coffeeWeightOut / coffeeWeightIn) BETWEEN :minRatio AND :maxRatio
        ORDER BY timestamp DESC
    """)
    fun getShotsByBrewRatioRange(minRatio: Double, maxRatio: Double): Flow<List<Shot>>
    
    /**
     * Get shots with extraction time in a specific range.
     */
    @Query("""
        SELECT * FROM shots 
        WHERE extractionTimeSeconds BETWEEN :minSeconds AND :maxSeconds
        ORDER BY timestamp DESC
    """)
    fun getShotsByExtractionTimeRange(minSeconds: Int, maxSeconds: Int): Flow<List<Shot>>
}

/**
 * Data class for shot statistics aggregation.
 */
data class ShotStatistics(
    val totalShots: Int,
    val avgWeightIn: Double,
    val avgWeightOut: Double,
    val avgExtractionTime: Double,
    val avgBrewRatio: Double
)