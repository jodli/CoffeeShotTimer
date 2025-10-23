package com.jodli.coffeeshottimer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jodli.coffeeshottimer.data.model.ShotRecommendation
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for shot recommendations.
 * Provides methods to store and query recommendation tracking data.
 */
@Dao
interface ShotRecommendationDao {

    /**
     * Insert a new shot recommendation.
     * Replaces existing recommendation if shotId already has one (one-to-one relationship).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recommendation: ShotRecommendation)

    /**
     * Get recommendation for a specific shot.
     * @param shotId The shot ID to get recommendation for
     * @return The recommendation, or null if none exists
     */
    @Query("SELECT * FROM shot_recommendations WHERE shotId = :shotId")
    suspend fun getByShotId(shotId: String): ShotRecommendation?

    /**
     * Get all shot recommendations as a flow.
     * Useful for coaching effectiveness analysis.
     */
    @Query("SELECT * FROM shot_recommendations ORDER BY timestamp DESC")
    fun getAllRecommendations(): Flow<List<ShotRecommendation>>

    /**
     * Get recommendations for shots of a specific bean.
     * @param beanId The bean ID to filter by
     * @return Flow of recommendations for that bean's shots
     */
    @Query(
        """
        SELECT sr.* FROM shot_recommendations sr
        INNER JOIN shots s ON sr.shotId = s.id
        WHERE s.beanId = :beanId
        ORDER BY sr.timestamp DESC
    """
    )
    fun getRecommendationsByBean(beanId: String): Flow<List<ShotRecommendation>>

    /**
     * Get all recommendations with their associated shot data.
     * Useful for comprehensive analytics.
     */
    @Query(
        """
        SELECT sr.* FROM shot_recommendations sr
        INNER JOIN shots s ON sr.shotId = s.id
        ORDER BY sr.timestamp DESC
    """
    )
    fun getAllRecommendationsWithShots(): Flow<List<ShotRecommendation>>

    /**
     * Count recommendations by follow status.
     * Useful for quick coaching effectiveness metrics.
     */
    @Query("SELECT COUNT(*) FROM shot_recommendations WHERE wasFollowed = :followed")
    suspend fun countByFollowStatus(followed: Boolean): Int

    /**
     * Get recommendations for a specific bean filtered by follow status.
     */
    @Query(
        """
        SELECT sr.* FROM shot_recommendations sr
        INNER JOIN shots s ON sr.shotId = s.id
        WHERE s.beanId = :beanId AND sr.wasFollowed = :followed
    """
    )
    suspend fun getRecommendationsByBeanAndFollowStatus(
        beanId: String,
        followed: Boolean
    ): List<ShotRecommendation>

    /**
     * Delete recommendation for a specific shot.
     * @param shotId The shot ID
     */
    @Query("DELETE FROM shot_recommendations WHERE shotId = :shotId")
    suspend fun deleteByShotId(shotId: String)

    /**
     * Delete all recommendations for shots of a specific bean.
     * Useful for bean deletion or cleanup.
     * @param beanId The bean ID
     */
    @Query(
        """
        DELETE FROM shot_recommendations
        WHERE shotId IN (SELECT id FROM shots WHERE beanId = :beanId)
    """
    )
    suspend fun deleteByBeanId(beanId: String)

    /**
     * Delete all recommendations.
     * Primarily for testing or data reset.
     */
    @Query("DELETE FROM shot_recommendations")
    suspend fun deleteAll()
}
