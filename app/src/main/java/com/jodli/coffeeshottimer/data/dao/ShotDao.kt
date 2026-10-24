package com.jodli.coffeeshottimer.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jodli.coffeeshottimer.data.model.Shot
import com.jodli.coffeeshottimer.domain.model.TastePrimary
import com.jodli.coffeeshottimer.domain.model.TasteSecondary
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
     * Get shots with pagination support (for performance optimization).
     */
    @Query("SELECT * FROM shots ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getShotsPaginated(limit: Int, offset: Int): List<Shot>

    /**
     * Get filtered shots with pagination support.
     */
    @Query(
        """
        SELECT * FROM shots 
        WHERE (:beanId IS NULL OR beanId = :beanId)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun getFilteredShotsPaginated(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?,
        limit: Int,
        offset: Int
    ): List<Shot>

    /**
     * Get count of filtered shots for pagination calculations.
     */
    @Query(
        """
        SELECT COUNT(*) FROM shots 
        WHERE (:beanId IS NULL OR beanId = :beanId)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
    """
    )
    suspend fun getFilteredShotsCount(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Int

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
     * Get shots within a date range.
     */
    @Query("SELECT * FROM shots WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getShotsByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): Flow<List<Shot>>

    /**
     * Get shots filtered by bean and date range.
     */
    @Query(
        """
        SELECT * FROM shots 
        WHERE (:beanId IS NULL OR beanId = :beanId)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
        ORDER BY timestamp DESC
    """
    )
    fun getFilteredShots(
        beanId: String?,
        startDate: LocalDateTime?,
        endDate: LocalDateTime?
    ): Flow<List<Shot>>

    /**
     * Insert a new shot.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShot(shot: Shot)

    /**
     * Delete a shot.
     */
    @Delete
    suspend fun deleteShot(shot: Shot)

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
     * Update taste feedback for a specific shot.
     */
    @Query(
        """
        UPDATE shots 
        SET tastePrimary = :tastePrimary, tasteSecondary = :tasteSecondary
        WHERE id = :shotId
    """
    )
    suspend fun updateTasteFeedback(
        shotId: String,
        tastePrimary: TastePrimary?,
        tasteSecondary: TasteSecondary?
    )
}
