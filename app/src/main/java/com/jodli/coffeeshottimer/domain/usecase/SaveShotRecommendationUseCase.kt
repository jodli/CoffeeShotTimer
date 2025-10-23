package com.jodli.coffeeshottimer.domain.usecase

import com.jodli.coffeeshottimer.data.dao.ShotRecommendationDao
import com.jodli.coffeeshottimer.data.model.ShotRecommendation
import com.jodli.coffeeshottimer.domain.exception.DomainException
import com.jodli.coffeeshottimer.domain.model.DomainErrorCode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for saving shot recommendation tracking data.
 * Stores recommendation data separately from the shot entity for better database design.
 */
@Singleton
class SaveShotRecommendationUseCase @Inject constructor(
    private val shotRecommendationDao: ShotRecommendationDao
) {

    /**
     * Save a shot recommendation with tracking data.
     *
     * @param shotId The ID of the shot this recommendation is for
     * @param recommendedGrindSetting The grind setting that was recommended
     * @param adjustmentDirection Direction of adjustment (FINER, COARSER, NO_CHANGE)
     * @param wasFollowed Whether the user followed the recommendation
     * @param confidenceLevel Confidence level of the recommendation (HIGH, MEDIUM, LOW)
     * @param reasonCode Code indicating why this recommendation was made
     * @param metadata Optional JSON metadata for extensibility
     * @return Result indicating success or failure
     */
    suspend fun saveRecommendation(
        shotId: String,
        recommendedGrindSetting: String,
        adjustmentDirection: String,
        wasFollowed: Boolean,
        confidenceLevel: String,
        reasonCode: String,
        metadata: String? = null
    ): Result<ShotRecommendation> {
        return try {
            val recommendation = ShotRecommendation(
                shotId = shotId,
                recommendedGrindSetting = recommendedGrindSetting,
                adjustmentDirection = adjustmentDirection,
                wasFollowed = wasFollowed,
                confidenceLevel = confidenceLevel,
                reasonCode = reasonCode,
                metadata = metadata
            )

            shotRecommendationDao.insert(recommendation)
            Result.success(recommendation)
        } catch (domainException: DomainException) {
            Result.failure(domainException)
        } catch (exception: IllegalStateException) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to save shot recommendation for shot $shotId",
                    exception
                )
            )
        }
    }

    /**
     * Get recommendation for a specific shot.
     *
     * @param shotId The shot ID
     * @return Result containing the recommendation or null if none exists
     */
    suspend fun getRecommendation(shotId: String): Result<ShotRecommendation?> {
        return try {
            val recommendation = shotRecommendationDao.getByShotId(shotId)
            Result.success(recommendation)
        } catch (exception: IllegalStateException) {
            Result.failure(
                DomainException(
                    DomainErrorCode.STORAGE_ERROR,
                    "Failed to get recommendation for shot $shotId",
                    exception
                )
            )
        }
    }
}
